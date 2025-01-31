package ni.vsbuild.packages

import ni.vsbuild.StringSubstitution

class Nipkg extends AbstractPackage {

   static final String PACKAGE_DIRECTORY = "nipkg"

   private static final String CONTROL_FILE_NAME = "control"
   private static final String INSTRUCTIONS_FILE_NAME = "instructions"
   private static final String CONTROL_DIRECTORY = "control"
   private static final String DATA_DIRECTORY = "data"

   def payloadMap
   def controlFile
   def instructionsFile
   def strategy

   Nipkg(script, packageInfo, lvVersion, strategy) {
      super(script, packageInfo, lvVersion)
      this.createPayloadMap(packageInfo)
      this.controlFile = packageInfo.get('control_file') ?: CONTROL_FILE_NAME
      this.instructionsFile = packageInfo.get('instructions_file') ?: INSTRUCTIONS_FILE_NAME
      this.strategy = strategy
   }

   void buildPackage(outputLocation) {
      script.echo "Staging files for $controlFile"
      stageFiles()

      script.echo "Building nipkg for $controlFile"
      def nipkgOutput = script.nipkgBuild(PACKAGE_DIRECTORY, PACKAGE_DIRECTORY)

      def outputDirectory = this.strategy.getOutputDirectory(script, outputLocation)
      script.echo "Copying files for $controlFile"
      script.copyFiles(PACKAGE_DIRECTORY, outputDirectory, [files: nipkgOutput])
   }

   String[] getConfigurationFiles() {
      return [this.controlFile, this.instructionsFile]
   }

   @NonCPS
   private void createPayloadMap(packageInfo) {
      def payloadDir = packageInfo.get('payload_dir')
      // Yes, I'm calling toString() on what appears to be a string, but is not actually
      // java.lang.String. Instead, the interpolated string is a groovy.lang.GString.
      // http://docs.groovy-lang.org/latest/html/documentation/index.html#_double_quoted_string
      // There appears to be a bug in Groovy's runtime argument overloading evaluation
      // that fails to find the key when a GString is passed to getAt() instead of a String
      // https://stackoverflow.com/questions/39145121/why-i-cannot-get-exactly-the-same-gstring-as-was-put-to-map-in-groovy
      def installDestination = packageInfo.get("${lvVersion.lvRuntimeVersion}_install_destination".toString()) ?: packageInfo.get('install_destination')

      if (payloadDir) {
         this.payloadMap = [(payloadDir): installDestination]
      } else {
         this.payloadMap = packageInfo.get('payload_map')
      }

      if (!this.payloadMap) {
         script.failBuild("Building an nipkg requires either 'payload_map', " +
               "or 'payload_dir' and 'install_destination' to be specified.")
      }
   }

   // This method is responsible for setting up the directory and file
   // structure required to build a File Package using nipkg.exe.
   // The structure is defined at the following link.
   // http://www.ni.com/documentation/en/ni-package-manager/18.5/manual/assemble-file-package/
   private void stageFiles() {
      if(script.fileExists(PACKAGE_DIRECTORY)) {
         script.bat "rmdir $PACKAGE_DIRECTORY /S /Q"
      }

      script.bat "mkdir \"$PACKAGE_DIRECTORY\\$CONTROL_DIRECTORY\" \"$PACKAGE_DIRECTORY\\$DATA_DIRECTORY\""

      createDebianFile()
      updateControlFile()
      updateInstructionsFile()

      stagePayload()
   }

   private void createDebianFile() {
      script.writeFile file: "$PACKAGE_DIRECTORY\\debian-binary", text: "2.0\n"
   }

   private void updateControlFile() {
      updateBuildFile(controlFile, CONTROL_DIRECTORY, CONTROL_FILE_NAME)
   }

   private void updateInstructionsFile() {
      updateBuildFile(instructionsFile, DATA_DIRECTORY, INSTRUCTIONS_FILE_NAME)
   }

   private void updateBuildFile(fileName, destination, outputFileName) {
      if(!script.fileExists(fileName)) {
         return
      }

      def fileText = script.readFile(fileName)
      def updatedText = updateVersionVariables(fileText)

      script.writeFile file: "$PACKAGE_DIRECTORY\\$destination\\$outputFileName", text: updatedText
   }

   // The plan is to enable automatic merging from main to
   // release or hotfix branch packages and not build packages
   // for any other branches, including main. The version must
   // be appended to the release or hotfix branch name after a
   // dash (-) or slash (/).
   private String updateVersionVariables(text) {
      def baseVersion = getBaseVersion()
      def fullVersion = getFullVersion()
      def majorVersion = getMajorVersion()
      def minorVersion = getMinorVersion()
      def updateVersion = getUpdateVersion()

      def additionalReplacements = [
         'major_version': majorVersion,
         'minor_version': minorVersion,
         'update_version': updateVersion,
         'major_minor_version': "$majorVersion.$minorVersion",
         'major_minor_update_version': "$majorVersion.$minorVersion.$updateVersion",
         'major_minor_update_build_version': fullVersion,
         'nipkg_version': fullVersion,
         'display_version': baseVersion,
         'quarterly_display_version': getQuarterlyDisplayVersion(),
      ]
      return StringSubstitution.replaceStrings(text, lvVersion, additionalReplacements)
   }

   private void stagePayload() {
      if (this.payloadMap.size() == 1) {
         def value = this.payloadMap.values().first()
         if (!value) {
            // If installDestination is not provided, build an
            // empty package (virtual package).
            // A virtual package is useful for defining package
            // relationships without requiring a package payload.
            return
         }
      }

      def finalMap = this.strategy.createNipkgPayloadMap(script, this.payloadMap, packageOutputDir)
      finalMap.each { payloadDir, installDestination ->
         def destination = updateVersionVariables(installDestination)
         script.copyFiles(payloadDir, "$PACKAGE_DIRECTORY\\$DATA_DIRECTORY\\$destination", [directoryExclusions: INSTALLER_DIRECTORY])
      }
   }
}
