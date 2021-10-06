call(int timeout) {
   try {
      deleteDir()
   }
   catch (CompositeIOException | FileSystemException e) {
      int sleepTime = 5
      int retryCount = timeout / sleepTime
      retry(retryCount) {
         sleep time: sleepTime unit: 'SECONDS'
         deleteDir()
      }
   }
}
