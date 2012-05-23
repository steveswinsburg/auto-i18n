# auto-i18n


A simple utility to automatically translate a Java properties file into any number of other languages. 

We all externalise our strings right? Now you can start adding other languages to your app automatically via
this simple app.

### Pre-requisites:

* Groovy 1.7 or later
* A properties file that you want to translate. At the moment this must be in English.

### Getting started:


* Configure the languages to translate to (and other settings) via config.properties
* Then run via:
  `groovy AutoI18n.groovy <properties_file>`

* For example:
  `groovy AutoI18n.groovy my.properties`


#### Note:

The translations provided via <strong>auto-i18n</strong> will never be as good as those that a native speaker can provide. 
Also, the placement of the placeholders in the properties eg {0} might need to be adjusted depending on the grammer of the target language.

All is not lost though, you'll have taken care of a lot of the mundane translations, and a simple copyedit should be all that is required.