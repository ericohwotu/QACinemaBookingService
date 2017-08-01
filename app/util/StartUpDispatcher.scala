package util

import com.google.inject.AbstractModule

class StartUpDispatcher extends AbstractModule {
  def configure(){
    bind(classOf[StartUpSchedular]).asEagerSingleton()
  }
}
