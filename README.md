auto-i18n
================================

Simple utility to automatically translate a Java properties file into any number of other languages. 

We all externalise our strings right? Now you can start adding other languages to your app.

 * Configure the languages to translate to (and other settings) via config.properties
 * you then need to provide a properties file as a base to translate from. At the moment this must be in English.
 * Then run via:
 * groovy AutoI18n.groovy <base_properties_file>
 *
 * For example:
 * groovy AutoI18n.groovy my.properties