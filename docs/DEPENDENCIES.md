# Dependency baseline and evidence

The current checked-in configuration was host-validated on 2026-10-21 with Gradle 9.4.1
and JDK 17.0.20.1. Assemble and unit-test tasks were UP-TO-DATE; Lint executed
successfully. The existing JUnit XML had 161 tests with zero failures/errors/skips.
Lint reported 22 warnings and 3 hints. This is not a tablet instrumentation, JNI
compatibility, complete licensing, or release validation.

| Component | Selection | Evidence / status |
|---|---|---|
| JDK | 17 | AGP 9.2 compatibility baseline; install distribution/patch recorded on host |
| AGP | 9.2.1 | Official 9.2 release notes list this stable patch |
| Gradle | 9.4.1 | Official AGP compatibility requirement |
| Kotlin / Compose compiler plugin | 2.3.10 project plugin | Pinned in root Gradle configuration; host build passed. Gradle's embedded Kotlin 2.3.0 is separate and is not evidence of a project plugin mismatch. |
| Compose BOM | 2026.04.01 | Official stable April release; deliberate older baseline to retain compileSdk 36 |
| Activity Compose | 1.13.0 | Official stable release notes |
| MuPDF fitz | 1.28.5 | Pinned project dependency and host build passed; vendor checksum authenticity, native device coverage and release licensing remain unaudited |
| JUnit | 4.13.2 | Official dependency information; maintenance-mode local JVM tests |
| SDK / Build Tools | 36 / 36.0.0 | AGP-supported baseline |
| minSdk | 29 | Android 10 |

Sources:
- https://developer.android.com/build/releases/agp-9-2-0-release-notes
- https://developer.android.com/build/migrate-to-built-in-kotlin
- https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.compose/2.3.10
- https://developer.android.com/blog/posts/whats-new-in-the-jetpack-compose-april-26-release
- https://developer.android.com/jetpack/androidx/releases/activity
- https://github.com/ArtifexSoftware/mupdf-android-fitz/blob/master/build.gradle
- https://junit.org/junit4/dependency-info.html

MuPDF Java APIs inspected at tag 1.28.5:
Document.openDocument(String), needsPassword(), isPDF(), countPages(), loadPage(int),
hasPermission(int), destroy(); Page.getBounds(), toStructuredText(), destroy();
StructuredText.asText(), destroy(); AndroidDrawDevice.drawPageFit(Page,int,int).
Git blob SHA Document.java: e9ebb51416b103731d0dc02673c6eabd1a5d6161
Git blob SHA AndroidDrawDevice.java: 44d3fe03879c0c0c9a10525c479a2b06c0b6cf08

Declaration checks and host compilation are not complete JNI/device execution checks.
The artifact script verifies archive structure and required classes, but does not prove
behavior or vendor checksum authenticity. Device ABI coverage remains a separate
acceptance check.

The genuine Gradle dependency lockfile is checked in at `app/gradle.lockfile`. Full
AndroidX/Kotlin/Gradle/JUnit transitive notice audits are pending.
MuPDF is AGPL-3.0-or-later in the inspected source; do not distribute an APK until
source/license/third-party obligations have been completed.
