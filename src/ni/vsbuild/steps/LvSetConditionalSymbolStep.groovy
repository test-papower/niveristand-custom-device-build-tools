package ni.vsbuild.steps

import ni.vsbuild.BuildConfiguration

class LvSetConditionalSymbolStep extends LvProjectStep {

   def symbol
   def trueValue
   def falseValue
   def condition

   LvSetConditionalSymbolStep(script, mapStep, lvVersion) {
      super(script, mapStep, lvVersion)
      this.symbol = mapStep.get('symbol')
      this.trueValue = mapStep.get('true_value')
      this.falseValue = mapStep.get('false_value')
      this.condition = mapStep.get('condition')
   }

   void executeStep(BuildConfiguration configuration) {
      def resolvedProject = resolveProject(configuration)
      def path = resolvedProject.get('path')
      
      def value = evaluateCondition() ? trueValue : falseValue

      script.lvSetConditionalSymbol(path, symbol, value, lvVersion)
   }
   
   private def evaluateCondition() {
      def expressionParts = condition.split()
      def leftSide = expressionParts[0]
      def operator = expressionParts[1]

      // All this nonsense because Jenkins security won't let me slice the list.
      // If https://issues.jenkins.io/browse/JENKINS-52036 is ever fixed (it says it
      // is, but the PR was closed without being merged), this whole section becomes
      // def rightSide = expressionParts[2..-1].join(' ')
      def rightSide = ''
      for (int i = 2; i < expressionParts.size(); i++) {
         rightSide = "$rightSide ${expressionParts[i]}"
      }
      rightSide = rightSide.trim()

      leftSide = this."$leftSide".toString()

      if(operator.equals("<")) {
         return leftSide < rightSide
      }
      if(operator.equals("<=")) {
         return leftSide <= rightSide
      }
      if(operator.equals(">")) {
         return leftSide > rightSide
      }
      if(operator.equals(">=")) {
         return leftSide >= rightSide
      }
      if(operator.equals("==")) {
         return leftSide == rightSide
      }
      if(operator.equals("!=")) {
         return leftSide != rightSide
      }
   }
}
