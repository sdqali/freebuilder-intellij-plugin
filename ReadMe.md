# FreeBuilder IntelliJ Plugin

This plugin makes it easy to apply [FreeBuilder]() annotations and create the necessary `Builder` classes on to a Java class.

The plugin's action can be invoked from the `Tools` menu or by searching for `Apply FreeBuilder` in the Actions search (Cmd-Shift-A).

Every invocation of the action results in the following:

* If the current class does not have a `FreeBuilder` annotation, it adds it.
* If the current class does not contain a child class named `Builder`, it creates it.
* Adds a `JsonDeserialiaze` annotation to the class if it is not already present.
* Adds a `JsonIgnoreProperties(ignoreUnknown = true)` annotation to the `Builder` child class if it is not already present.
* Forces a project rebuild so that the `Builder` class gets resolved.
