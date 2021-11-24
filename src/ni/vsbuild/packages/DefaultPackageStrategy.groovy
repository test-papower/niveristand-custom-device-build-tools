package ni.vsbuild.packages

import ni.vsbuild.Architecture

class DefaultPackageStrategy implements PackageStrategy {

   def lvVersion

   DefaultPackageStrategy(lvVersion) {
      this.lvVersion = lvVersion
   }

   def filterPackageCollection(packageCollection) {
      // Get all packages with a configuration where
      // multi_bitness is not defined OR
      //    if multi_bitness is defined AND the current LabVIEW version
      //    does not match any of the versions specified for multi-bitness packaging.
      def filteredCollection = packageCollection.findAll { !(it.get('multi_bitness')) \
                                                         || ((it.get('multi_bitness_versions')) \
                                                            && !(it.get('multi_bitness_versions').contains(lvVersion.lvRuntimeVersion)))
      }

      // Remove all remaining packages with a configuration where
      // bitness is specified AND
      //    the current bitness does not match the specified bitness OR
      //    bitness_versions is specified AND the current version is not found.
      //
      // If bitness matches and bitness_versions is not specified, assume bitness
      // applies to all LabVIEW versions.
      filteredCollection.removeAll { it.get('bitness') \
                                    && (Architecture.bitnessToArchitecture(it.get('bitness') as Integer) != lvVersion.architecture \
                                       || ((it.get('bitness_versions')) \
                                          && !(it.get('bitness_versions').contains(lvVersion.lvRuntimeVersion))))
      }

      return filteredCollection
   }

   def getOutputDirectory(script, outputDir) {
      return outputDir
   }

   def createNipkgPayloadMap(script, payloadMap, outputDir) {
      return payloadMap
   }
}
