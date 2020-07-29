package ni.vsbuild.steps

import ni.vsbuild.BuildConfiguration

class GitResetStep extends AbstractStep {

   def parameters

   GitResetStep(script, mapStep) {
      super(script, mapStep, lvVersion)
      this.parameters = mapStep.get('parameters')
   }

   void executeStep(BuildConfiguration configuration) {
      script.gitReset(parameters)
   }
}
