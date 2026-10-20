# Nexora
## Student Skills & Placement Tracker

Nexora is an Android application designed to help college students
organize their skills, projects, certifications, and coding practice
while tracking their progress toward placement eligibility.

The application also provides coordinator-oriented features for
managing company information and placement announcements.

## 1. Objectives

- Maintain student profile information.
- Organize skills, projects, and certifications.
- Record coding practice statistics.
- View company requirements and check eligibility.
- Track placement readiness through a dashboard.
- Allow coordinators to manage company information and announcements.
- Protect application data through Firebase Authentication and
  Cloud Firestore security rules.

## 2. Features

### Student Features

- Student registration and login.
- Student profile creation, viewing, and editing.
- Skill management.
- Project management.
- Certification management.
- Coding practice statistics.
- Company information viewing.
- Eligibility checking against company requirements.
- Placement readiness dashboard.
- Placement announcement viewing.

### Coordinator Features

- Coordinator access to the management interface.
- Company information management.
- Placement announcement management.

Coordinator access depends on the application's role configuration
and the deployed Firestore security rules.

## 3. Technology Stack

| Component | Technology |
|---|---|
| Platform | Android |
| Programming language | Java |
| User interface | XML |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Build system | Gradle |
| Testing | JUnit and Firestore Emulator tests |

## 4. Application Architecture

The application uses a layered structure to separate screen logic,
data access, models, and business calculations.

- Activities handle user interaction and screen presentation.
- Repository classes handle access to Firebase data.
- Model classes represent application data.
- Utility classes perform eligibility evaluation and readiness
  calculations.
- Firebase Authentication manages user sessions.
- Cloud Firestore stores application records and enforces
  database security rules.

## 5. Main Modules

### Student Profile

Stores student information and supports profile viewing and editing.

### Skills and Projects

Allows students to organize their skills and project records.

### Certifications and Coding Statistics

Provides records for certifications and coding practice.

### Company Eligibility

Compares student information with company requirements, including
CGPA, skills, and project requirements.

Eligibility results are informational and depend on the data
recorded in the application.

### Placement Readiness

Displays a calculated progress score based on the information
available in the student's profile and records.

The readiness score is an application-defined progress indicator.
It is not a probability of receiving a placement offer.

### Placement Announcements

Allows coordinators to manage announcements and students to view them.

## 6. Prerequisites

To build and run the project, you need:

- Android Studio.
- A compatible Java Development Kit supported by the project's
  Gradle and Android Gradle Plugin versions.
- Android SDK components required by the project.
- A Firebase project.
- Firebase Authentication with Email/Password enabled.
- Cloud Firestore configured for the Firebase project.

## 7. Firebase Configuration

1. Open the project in Android Studio.
2. Create or select the Firebase project used for the application.
3. Register the Android application using its configured package name.
4. Download the Firebase Android configuration file.
5. Place the file at:

   `app/google-services.json`

6. Enable Email/Password authentication in Firebase Authentication.
7. Configure Cloud Firestore.
8. Review the Firestore rules before deployment.

The file `app/google-services.json` is intentionally excluded from
Git tracking. Obtain it from your own authorized Firebase project
configuration when setting up a local development environment.

Never commit service-account credentials, private keys, or other
server-side secrets to the repository.

## 8. Build the Application

On Windows, open PowerShell in the project root.

Build the debug APK:

```powershell
.\gradlew :app:assembleDebug