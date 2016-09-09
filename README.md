OAuth Resource Server Demo
=======================================================

## Objective


This sample shows an OAuth RS (or microservice).

It relies on Silhouette and Oauth2 for caller authorization.

## Usage

1. Start Talend IAM OIDC 

   * install gradle
   * customize ~/.gradle/gradle.properties as stated in https://in.talend.com/13995845
   * clone https://github.com/Talend/platform-services
   * cd platform-services/iam
   * gradle clean buildDocker
   * cd idp
   * docker-compose -f build/docker-compose.yml up
   
1. Register your OAuth Application Client in IAM

   * create an end-user (i.e. alice/alice), as in https://in.talend.com/14681120
   * create the OAuth clientId and clientSecret (as in https://in.talend.com/14681120)
   
     with the following info :
     
   | Name             | Value                       |
   | ---------------- | --------------------------- |
   | Application Name | Sample Play RS Application  |
   | Redirect URL     | http://localhost:9999/login |
   
   Copy the created clientId and clientSecret, you'll need them in the following step.
   
1. Configure sample-ac application

   Note : I should change the keys, this isn't the OIDC Application Client !

   
     conf/silhouette.conf with the following info :
     
   | Name             | Value                       |
   | ---------------- | --------------------------- |
   | silhouette.oidc.clientId | <value returned by previous step>     |
   | silhouette.oidc.clientSecret | <value returned by previous step>     |
   | silhouette.oidc.tokenIntrospectUri | OAuth AS introspect endpoint     |
      
1. Start sample-rs application on port 9001
1. Test it :
   
   * http://localhost:9001/rs
   
     Just returns Hello
     
   * http://localhost:9001/rs/secure
   
     This is the secured endpoint.
     
     This returns a 401 if you HTTP header doesn't contain a valid bearer Access Token.
     
     Returns a 403 is your AT doesn't have admin scope.
