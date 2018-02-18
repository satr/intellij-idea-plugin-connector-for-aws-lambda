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
* Open the tool window ![](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/sources/resources/icons/iconConnector.png?raw=true) "Connector for AWS Lambda".
* Refresh ![Refresh](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/sources/resources/icons/iconRefresh.png?raw=true) lists with AWS Lambda functions, JAR-artifacts, regions and profiles.
* Select in the list an AWS Lambda function to update it or run a test for it. 
* Update the function with the button ![Update Function](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/sources/resources/icons/iconUpdateFunction.png?raw=true) (do not forget first to build an updated function). 
* Type an input for a function test or load it with the button ![Open Test Input](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/sources/resources/icons/iconOpenFunctionInputFile.png?raw=true) from a json-file. 
* Hit the button ![Run Function](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/sources/resources/icons/iconRunFunctionTest.png?raw=true) to run the test for a selected function. 
* Track activities in the Log window. Clean the log with the button ![Clean log](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/sources/resources/icons/iconClearLog.png?raw=true)
* Set a proxy properties in the IDEA settings and setup these values in the plugin with the button ![Update proxy settings from IDEA settings](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/sources/resources/icons/iconUpdateProxySettings.png?raw=true)

![](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/docs/images/intellij-idea-plugin-connector-for-aws-lambda-01.png?raw=true)

![](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/docs/images/intellij-idea-plugin-connector-for-aws-lambda-02.png?raw=true)

![](https://github.com/satr/intellij-idea-plugin-connector-for-aws-lambda/blob/master/docs/images/intellij-idea-plugin-connector-for-aws-lambda-03.png?raw=true)

Amazon Web Services, and AWS Lambda are trademarks of Amazon.com, Inc. or its affiliates in the United States and/or other countries.
