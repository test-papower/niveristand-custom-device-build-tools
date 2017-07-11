class CommonBuilder implements Serializable {
  
  private static final String EXPORT_DIR = 'export'
  private static final String BUILD_STEPS_LOCATION = 'vars/buildSteps.groovy'
  
  private def script
  private String[] lvVersions
  private String sourceVersion
  private String archiveLocation
  private def buildSteps
  
  public CommonBuilder(script, lvVersions, sourceVersion) {
    this.script = script
    this.lvVersions = lvVersions
    this.sourceVersion = sourceVersion
  }
  
  public def loadBuildSteps() {
    this.buildSteps = this.script.load BUILD_STEPS_LOCATION
    return this.buildSteps
  }
  
  public boolean setup() {
    // Ensure the VIs for executing scripts are in the workspace
    this.script.syncCommonbuild('dynamic-load')
    
    this.script.echo 'Syncing dependencies.'
    this.buildSteps.syncDependencies()
  }
  
  public boolean runUnitTests() {
    //Make sure correct dependencies are loaded to run unit tests
    this.preBuild(this.sourceVersion)
  }
  
  public boolean build() {
    this.script.bat "mkdir $EXPORT_DIR"
    
    lvVersions.each{lvVersion->
      echo "Building for LV Version $lvVersion..."
      this.preBuild(lvVersion)
      this.buildSteps.build(lvVersion)
      
      //Move build output to versioned directory
      bat "move \"${this.buildSteps.BUILT_DIR}\" \"$EXPORT_DIR\\$lvVersion\""
      echo "Build for LV Version $lvVersion complete."
  }
  
  public boolean archive() {
  }
  
  public boolean deploy() {
  }
  
  public boolean publish() {
  }
  
  private def preBuild(lvVersion) {
    this.buildSteps.prepareSource(lvVersion)
    this.buildSteps.setupLv(lvVersion)
  }
}
