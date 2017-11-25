# intellij-idea-plugin-connector-for-aws-lambda
The plugin for the IntelliJ IDEA: connector for AWS Lambda functions.

View and update functions powered by AWS Lambda with JAR-artifact.

Please read more about AWS Lambda on the following site: https://aws.amazon.com/lambda

Plugin on IntelliJ site: https://plugins.jetbrains.com/plugin/9886-connector-for-aws-lambda

Latest releases and pre-releases: https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/releases

* Install [AWS CLI](https://aws.amazon.com/cli/) and configure credentials for a user with permits to administer AWS Lambda functions. 
  * Command to create a credentials profile
  ```
  aws configure --profile YOUR-PROFILE-NAME
  ```
* Install the plugin. 
* Open a project with an AWS Lambda function. 
* Configure a JAR-artifact for the project. 
* Open the tool window "Connector for AWS Lambda". 
* Refresh lists with AWS Lambda functions, JAR-artifacts, regions and profiles.
* Select in the list an AWS Lambda function to update it or run a test for it. 
* Hit the button "Update Function". 
* Type an input for a function test or hit the "Open" button to load it from a json-file. 
* Hit the "Run" button to run the test for a selected function. 
* Track activities in the Log window.

![](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/docs/intellij-idea-plugin-connector-for-aws-lambda-01.png?raw=true)

![](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/docs/intellij-idea-plugin-connector-for-aws-lambda-02.png?raw=true)

Amazon Web Services, and AWS Lambda are trademarks of Amazon.com, Inc. or its affiliates in the United States and/or other countries.
