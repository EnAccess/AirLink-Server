Please visit the AirLink landing page for an overview:
https://www.enaccess.org/airlink

# Rule Chains to add to new AirLink tenant
In an AirLink server, the Business Logic Rule chain should be added first and then the Root rule chain, via the UI or API calls. The newly added Root rule chain should be marked root, and that's sufficient to setup the server to accept AirLink inputs.

# Postman collection to test AirLink tenant
We have a Postman collection at the root level of this repository that needs to be paired with the set of environment variable values that are specific to each tenant

# Nexus KeyCode Rule Node for Add, Set and Unlock Devices
This node is to be added to the rule chain for creations of tokens for devices.

# How to install the build and install the jar file
````
https://thingsboard.io/docs/user-guide/contribution/rule-node-development/
````

# Jar file location

````
jar/code-generator-1.0.0-token-generation-nodes.jar
````
