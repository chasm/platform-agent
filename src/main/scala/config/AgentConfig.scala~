package com.munat.pagent

import com.biosimilarity.lift.lib._

object AgentConfig extends ConfigurationTrampoline {
  var _configFileName: Option[String] = None
  
  override def configurationDefaults: ConfigurationDefaults = {
    AgentDefaults.asInstanceOf[ConfigurationDefaults]
  }
  
  def configFileName: Option[String] = _configFileName
  
  def setConfigFileName(fileName: String) = {
    _configFileName = Some(fileName)
  }
}
