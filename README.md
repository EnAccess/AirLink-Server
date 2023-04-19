<p align="center">
  <a href="https://github.com/EnAccess/AirLink-Server">
    <img
      src="https://enaccess.org/wp-content/uploads/2023/04/Airlink-Graphics-GitHub-2240-%C3%97-800-.svg"
      alt="AirLink-Server"
      width="640"
    >
  </a>
</p>
<p align="center">
    <em>AirLink uses financed phones as relay-extensions of the internet in remote areas, to extend productive asset data coverage in even the most rural communities. By introducing open-standards communications, AirLink allows customers’ phones and PAYGo assets to communicate with each other by using widely available, standard low-energy Bluetooth connectivity.</em>
</p>
<p align="center">
  <img
    alt="Project Status"
    src="https://img.shields.io/badge/Project%20Status-stable-green"
  >
  <a href="https://github.com/EnAccess/AirLink-Server/blob/main/LICENSE" target="_blank">
    <img
      alt="License"
      src="https://img.shields.io/github/license/EnAccess/AirLink-Server"
    >
  </a>
</p>

---

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
