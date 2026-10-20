/**
 * Firestore Security Rules Tests
 * Student Skills & Placement Tracker
 *
 * Prerequisites (run once):
 *   npm install --save-dev @firebase/rules-unit-testing mocha
 *   npx firebase login       (or firebase login --no-localhost)
 *   npx firebase init emulators   (select Firestore)
 *
 * Run:
 *   npx firebase emulators:exec --only firestore \
 *       "npx mocha tests/firestore/rules.test.js"
 *
 * Or interactively:
 *   npx firebase emulators:start --only firestore
 *   npx mocha tests/firestore/rules.test.js
 */

"use strict";

const { readFileSync }                     = require("fs");
const { resolve }                          = require("path");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
}                                          = require("@firebase/rules-unit-testing");

// ── Constants ──────────────────────────────────────────────────────────────
const PROJECT_ID   = "demo-sspt-test";           // any demo- project id works
const RULES_PATH   = resolve(__dirname, "../../firestore.rules");

const STUDENT_UID  = "student-uid-1";
const OTHER_UID    = "student-uid-2";
const COORD_UID    = "coordinator-uid-1";

// Minimal valid profile written by RegisterActivity / StudentRepository.createProfile()
function studentProfile(uid) {
  return {
    uid,
    name:       "Test Student",
    email:      `${uid}@college.edu`,
    department: "CSE",
    year:       2,
    cgpa:       8.5,
    role:       "student",
    createdAt:  new Date(),
    updatedAt:  new Date(),
  };
}

function coordinatorProfile(uid) {
  return { ...studentProfile(uid), role: "coordinator", email: `${uid}@admin.edu` };
}

// ── Test environment setup ─────────────────────────────────────────────────
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(RULES_PATH, "utf8"),
      host:  "127.0.0.1",
      port:  8080,
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

// Helper: seed Firestore data directly (bypasses rules).
// withSecurityRulesDisabled passes a RulesTestContext, not a Firestore
// instance, so we unwrap it here so every call site receives a plain db.
async function seed(callback) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await callback(context.firestore());
  });
}

// Helper: unauthenticated client
function unauth()            { return testEnv.unauthenticatedContext();            }
// Helper: authenticated as a given uid
function auth(uid)           { return testEnv.authenticatedContext(uid);           }

// ── Convenience db references ──────────────────────────────────────────────
const db = (ctx) => ctx.firestore();

// ══════════════════════════════════════════════════════════════════════════
// 1. Unauthenticated access — everything must be denied
// ══════════════════════════════════════════════════════════════════════════
describe("Unauthenticated access", () => {
  before(async () => {
    await seed(async (db) => {
      await db.collection("students").doc(STUDENT_UID).set(studentProfile(STUDENT_UID));
      await db.collection("companies").doc("c1").set({ companyId: "c1", companyName: "Acme" });
      await db.collection("announcements").doc("a1").set({ announcementId: "a1", title: "Drive" });
    });
  });

  it("cannot read a student profile",           async () =>
    assertFails(unauth().firestore().collection("students").doc(STUDENT_UID).get()));

  it("cannot write a student profile",          async () =>
    assertFails(unauth().firestore().collection("students").doc("x").set({ uid: "x" })));

  it("cannot read companies",                   async () =>
    assertFails(unauth().firestore().collection("companies").doc("c1").get()));

  it("cannot write companies",                  async () =>
    assertFails(unauth().firestore().collection("companies").doc("c2").set({})));

  it("cannot read announcements",               async () =>
    assertFails(unauth().firestore().collection("announcements").doc("a1").get()));

  it("cannot write announcements",              async () =>
    assertFails(unauth().firestore().collection("announcements").doc("a2").set({})));

  it("cannot read a student subcollection",     async () =>
    assertFails(unauth().firestore()
      .collection("students").doc(STUDENT_UID).collection("skills").get()));
});

// ══════════════════════════════════════════════════════════════════════════
// 2. Student profile creation
// ══════════════════════════════════════════════════════════════════════════
describe("Student profile creation", () => {
  beforeEach(async () => {
    await testEnv.clearFirestore();
  });

  it("can create own profile with role='student'", async () =>
    assertSucceeds(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID)
        .set(studentProfile(STUDENT_UID))
    )
  );

  it("cannot create another student's profile", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(OTHER_UID)
        .set(studentProfile(OTHER_UID))
    )
  );

  it("cannot self-assign role='coordinator' at registration", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID)
        .set({ ...studentProfile(STUDENT_UID), role: "coordinator" })
    )
  );

  it("cannot self-assign role='admin' at registration", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID)
        .set({ ...studentProfile(STUDENT_UID), role: "admin" })
    )
  );

  it("cannot create profile with mismatched uid field", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID)
        .set({ ...studentProfile(STUDENT_UID), uid: "spoofed-uid" })
    )
  );

  it("cannot create profile with unexpected extra field", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID)
        .set({ ...studentProfile(STUDENT_UID), superUser: true })
    )
  );
});

// ══════════════════════════════════════════════════════════════════════════
// 3. Student profile updates
// ══════════════════════════════════════════════════════════════════════════
describe("Student profile updates", () => {
  before(async () => {
    await seed(async (db) => {
      await db.collection("students").doc(STUDENT_UID).set(studentProfile(STUDENT_UID));
      await db.collection("students").doc(OTHER_UID).set(studentProfile(OTHER_UID));
    });
  });

  it("can update allowed profile fields (name, department, year, cgpa, updatedAt)", async () =>
    assertSucceeds(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID).update({
        name:       "Updated Name",
        department: "ECE",
        year:       3,
        cgpa:       9.0,
        updatedAt:  new Date(),
      })
    )
  );

  it("cannot change own uid", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID).update({
        uid:       "new-uid",
        updatedAt: new Date(),
      })
    )
  );

  it("cannot change own role to coordinator", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID).update({
        role:      "coordinator",
        updatedAt: new Date(),
      })
    )
  );

  it("cannot change own email", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID).update({
        email:     "newemail@example.com",
        updatedAt: new Date(),
      })
    )
  );

  it("cannot change own createdAt", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID).update({
        createdAt: new Date(),
        updatedAt: new Date(),
      })
    )
  );

  it("cannot update another student's profile", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(OTHER_UID).update({
        name:      "Hacked",
        updatedAt: new Date(),
      })
    )
  );

  it("cannot delete own profile", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID).delete()
    )
  );
});

// ══════════════════════════════════════════════════════════════════════════
// 4. Student subcollections — owner-only
// ══════════════════════════════════════════════════════════════════════════
describe("Student subcollections", () => {
  before(async () => {
    await seed(async (db) => {
      await db.collection("students").doc(STUDENT_UID).set(studentProfile(STUDENT_UID));
      await db.collection("students").doc(OTHER_UID).set(studentProfile(OTHER_UID));
    });
  });

  const subcollections = ["skills", "projects", "certifications", "coding"];

  for (const sub of subcollections) {
    it(`${sub}: owner can write`, async () =>
      assertSucceeds(
        db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID)
          .collection(sub).doc("doc1").set({ userId: STUDENT_UID, test: true })
      )
    );

    it(`${sub}: owner can read`, async () =>
      assertSucceeds(
        db(auth(STUDENT_UID)).collection("students").doc(STUDENT_UID)
          .collection(sub).get()
      )
    );

    it(`${sub}: other student cannot read`, async () =>
      assertFails(
        db(auth(OTHER_UID)).collection("students").doc(STUDENT_UID)
          .collection(sub).get()
      )
    );

    it(`${sub}: other student cannot write`, async () =>
      assertFails(
        db(auth(OTHER_UID)).collection("students").doc(STUDENT_UID)
          .collection(sub).doc("evil").set({ userId: OTHER_UID })
      )
    );
  }
});

// ══════════════════════════════════════════════════════════════════════════
// 5. Companies — student read, coordinator write
// ══════════════════════════════════════════════════════════════════════════
describe("Companies", () => {
  before(async () => {
    await seed(async (db) => {
      await db.collection("students").doc(STUDENT_UID).set(studentProfile(STUDENT_UID));
      await db.collection("students").doc(COORD_UID).set(coordinatorProfile(COORD_UID));
      await db.collection("companies").doc("c1").set({ companyId: "c1", companyName: "Acme" });
    });
  });

  it("authenticated student can read companies", async () =>
    assertSucceeds(db(auth(STUDENT_UID)).collection("companies").get())
  );

  it("unauthenticated user cannot read companies", async () =>
    assertFails(unauth().firestore().collection("companies").get())
  );

  it("student cannot create a company", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("companies").doc("c2").set({ companyId: "c2" })
    )
  );

  it("student cannot update a company", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("companies").doc("c1").update({ companyName: "Evil" })
    )
  );

  it("student cannot delete a company", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("companies").doc("c1").delete()
    )
  );

  it("coordinator can create a company", async () =>
    assertSucceeds(
      db(auth(COORD_UID)).collection("companies").doc("c3")
        .set({ companyId: "c3", companyName: "Beta Corp", role: "SDE",
               minimumCGPA: 7.5, requiredSkills: ["Java"], minimumProjects: 1,
               location: "Bengaluru", createdAt: new Date(), updatedAt: new Date() })
    )
  );

  it("coordinator can update a company", async () =>
    assertSucceeds(
      db(auth(COORD_UID)).collection("companies").doc("c1")
        .update({ companyName: "Acme Updated", updatedAt: new Date() })
    )
  );

  it("coordinator can delete a company", async () =>
    assertSucceeds(
      db(auth(COORD_UID)).collection("companies").doc("c3").delete()
    )
  );
});

// ══════════════════════════════════════════════════════════════════════════
// 6. Announcements — student read, coordinator write
// ══════════════════════════════════════════════════════════════════════════
describe("Announcements", () => {
  before(async () => {
    await seed(async (db) => {
      await db.collection("students").doc(STUDENT_UID).set(studentProfile(STUDENT_UID));
      await db.collection("students").doc(COORD_UID).set(coordinatorProfile(COORD_UID));
      await db.collection("announcements").doc("a1")
        .set({ announcementId: "a1", title: "Drive", description: "Test",
               companyName: "Acme", date: new Date(),
               createdAt: new Date(), updatedAt: new Date() });
    });
  });

  it("authenticated student can read announcements", async () =>
    assertSucceeds(db(auth(STUDENT_UID)).collection("announcements").get())
  );

  it("student cannot publish an announcement", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("announcements").doc("a2").set({ title: "Fake" })
    )
  );

  it("student cannot delete an announcement", async () =>
    assertFails(
      db(auth(STUDENT_UID)).collection("announcements").doc("a1").delete()
    )
  );

  it("coordinator can publish an announcement", async () =>
    assertSucceeds(
      db(auth(COORD_UID)).collection("announcements").doc("a3")
        .set({ announcementId: "a3", title: "New Drive", description: "Details",
               companyName: "Beta", date: new Date(),
               createdAt: new Date(), updatedAt: new Date() })
    )
  );

  it("coordinator can delete an announcement", async () =>
    assertSucceeds(
      db(auth(COORD_UID)).collection("announcements").doc("a3").delete()
    )
  );
});

// ══════════════════════════════════════════════════════════════════════════
// 7. Missing profile cannot obtain coordinator privileges
// ══════════════════════════════════════════════════════════════════════════
describe("Missing profile — privilege escalation prevention", () => {
  before(async () => {
    await testEnv.clearFirestore();
    // ORPHAN_UID has a Firebase Auth account but NO Firestore document
    // (simulates the orphaned-account scenario fixed in MainActivity.java)
    await seed(async (db) => {
      await db.collection("companies").doc("c1").set({ companyId: "c1", companyName: "Acme" });
    });
  });

  const ORPHAN_UID = "orphan-uid-no-profile";

  it("authenticated orphan cannot write to companies", async () =>
    assertFails(
      db(auth(ORPHAN_UID)).collection("companies").doc("c2").set({ companyId: "c2" })
    )
  );

  it("authenticated orphan cannot write announcements", async () =>
    assertFails(
      db(auth(ORPHAN_UID)).collection("announcements").doc("a2").set({ title: "X" })
    )
  );

  it("authenticated orphan cannot read other students' profiles", async () =>
    assertFails(
      db(auth(ORPHAN_UID)).collection("students").doc(STUDENT_UID).get()
    )
  );
});

