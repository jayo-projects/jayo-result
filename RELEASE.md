## Release
* verify the current version you want to release in gradle.properties
* verify you are using SSH with GIT
* use Temurin 17 as the project JDK
* do **publish** task
* go to *build/repos/releases*
* remove the "maven-metadata.xml" (and all files in the same directory)
* zip the **dev** dir. Save it with a name like *jayo-result-0.1.0-alpha1.zip*.
* upload manually on https://central.sonatype.com/publishing (jayo account)
  * do **Publish Component**
  * Deployment name → use release name *jayo-result-X.Y.Z*, use the same name as the zip.
  * Click on **Publish Component**, **Refresh**. Check artifacts are ok, then **Publish**
  * Refresh again after several minutes, deployment status must be "PUBLISHED"
* do **release** task (for minor release, press Enter for suggested versions: release version = current,
  new version = current + 1)
