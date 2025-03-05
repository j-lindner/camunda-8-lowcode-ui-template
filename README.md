[![Community Extension](https://img.shields.io/badge/Community%20Extension-An%20open%20source%20community%20maintained%20project-FF4700)](https://github.com/camunda-community-hub/community)
![Compatible with: Camunda Platform 8](https://img.shields.io/badge/Compatible%20with-Camunda%20Platform%208-0072Ce)
[![](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)

# Low-code Solution Template for Camunda Platform 8 using React, Java and Spring Boot

We've had some customers who would like to offer some "citizen dev tools" to their business users :
- build Forms using drag'n drop tools like [form-js](https://bpmn.io/toolkit/form-js/) but with more components.
- build/adapt mail templates

The goal of this project is to show how to build such a solution with [React](https://reactjs.org/), an [extended form-js](https://github.com/camunda-community-hub/extended-form-js), [Spring-Boot](https://spring.io/projects/spring-boot), [spring-zeebe](https://github.com/camunda-community-hub/spring-zeebe), [tasklist-client](https://github.com/camunda-community-hub/camunda-tasklist-client-java) and [operate-client](https://github.com/camunda-community-hub/camunda-operate-client-java)

:information_source: DMN & FEEL tester have been externalized to [camunda-8-dev-tooling](https://github.com/camunda-community-hub/camunda-8-dev-tooling)
:information_source: An example of bpmn-js integration is available in [camunda-custom-operation-tools](https://github.com/camunda-community-hub/camunda-custom-operation-tools)

## Repository content

This repository contains a Java application template for Camunda Platform 8 using Spring Boot
and a [docker-compose.yaml](docker-compose.yaml) file for local development. For production setups we recommend to use our [helm charts](https://docs.camunda.io/docs/self-managed/platform-deployment/kubernetes-helm/).

- [Documentation](https://docs.camunda.io)
- [Camunda Platform SaaS](https://camunda.io)
- [Getting Started Guide](https://github.com/camunda/camunda-platform-get-started)
- [Releases](https://github.com/camunda/camunda-platform/releases)
- [Helm Charts](https://helm.camunda.io/)
- [Zeebe Workflow Engine](https://github.com/camunda/zeebe)
- [Contact](https://docs.camunda.io/contact/)

The Spring Boot Java application includes an example Tasklist [React front-end](src/main/react/tasklist/). Run `make buildfront` from the project root, start the Spring Boot app (`make run` or `mvnw spring-boot:run`), and then browse to http://localhost:8080.

This front-end relies on a [customized version of @bpmnio/form-js](https://github.com/camunda-community-hub/extended-form-js).

If needed, you can also run the [React front-end](src/main/react/tasklist/) independent of the spring boot app. To do so, run `npm run start` to start a nodejs server serving the react app over port 3000. You can also use the `make runfront`

## Using this template

Fork [this repository](https://github.com/camunda-community-hub/camunda-8-lowcode-ui-template) on GitHub
and rename/refactor the following artifacts:

* `groupId`, `artifactId`, `name`, and `description` in [pom.xml](pom.xml)
* `process/@id` and `process/@name` in [src/main/resources/models/camunda-process.bpmn](src/main/resources/models/camunda-process.bpmn)
* `ProcessConstansts#BPMN_PROCESS_ID` in [src/main/java/org/example/camunda/process/solution/ProcessConstants.java](src/main/java/org/example/camunda/process/solution/ProcessConstants.java)
* Java package name, e.g. `org.example.camunda.process.solution.*`

By forking this project, you can stay connected to improvements that we do to this template and simply pull updates into your fork, e.g. by using GitHub's Web UI or the following commands:

```sh
git remote add upstream git@github.com:camunda-community-hub/camunda-8-lowcode-ui-template.git
git pull upstream main
git push
```

### Forking to GitLab
```
gh repo clone camunda-community-hub/camunda-8-lowcode-ui-template new-project-folder
cd new-project-folder
git remote set-url origin git@gitlab.com:new-project/new-repo
```

## First steps with the application

The application requires a running Zeebe engine.
You can run Zeebe locally using the instructions below for Docker Compose
or have a look at our
[recommended deployment options for Camunda Platform](https://docs.camunda.io/docs/self-managed/platform-deployment/#deployment-recommendation.).

Before starting the app go to http://localhost:8084/applications/
and create application of type M2M with read/write access to Operate & Tasklist
and set `identity.clientId` and `identity.clientSecret` in [application.yaml](https://github.com/camunda-community-hub/camunda-8-lowcode-ui-template/blob/acf53b204efcae9c3136190ff2ce1808981a4375/src/main/resources/application.yaml#L17-L18).

Run the application via
```
./mvnw spring-boot:run
```

UI [http://localhost:8080/](http://localhost:8080/)
Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
> :information_source: The docker-compose file in this repository uses the latest [compose specification](https://docs.docker.com/compose/compose-file/), which was introduced with docker compose version 1.27.0+. Please make sure to use an up-to-date docker compose version.

The first time you use the project, you should be able to connect with demo/demo to create new forms, mail templates and manage your user organization.

When you start the application for the first time, an "ACME" organization is created for you with a single user demo/demo with admin rights. When you access the landing page, you'll be able to access the "admin" section where you can [manage forms](https://github.com/camunda-community-hub/extended-form-js), [mail templates](https://github.com/camunda-community-hub/thymeleaf-feel) and your organization.

## Oauth2 integration
If you want to use this application with an Oauth provider, you can just configure the spring.security.oauth2 properties in the application.yaml file.

```yaml
spring:
  security.oauth2:
    enabled: true
    client:
      registration.customTaskList:
        client-id: customTasklist
        client-secret: XXX
        authorization-grant-type: authorization_code
        scope: openid, profile
        redirect-uri: http://localhost:8080/login/oauth2/code/customTaskList
      provider.customTaskList.issuer-uri: http://localhost:18080/auth/realms/camunda-platform
```

The first property is to enable it.

You need to configure the client properly into your IDP. In Keycloak, if you want to use permissions and groups, you should add mappers in to client scope. Pay attention that roles are expected to be prefixed with ROLE_

## Forms
You can create forms with 3 different approaches :
- Embedded forms into your process definition (classic approach)
- Create Extended Forms from the admin application. Form name should match the formKey. Forms will be stored in the workspace/forms folder.
- Create custom react forms. From the react application, in the forms folder, create your custom forms. You should then reference them in the customForms map in the forms/index.ts file.

Translations will be managed from the backend in the 2 first approaches. Input labels should be references in the Forms translations.
In the 3rd case, translation will be managed from the front-end and translations should be added in the "siteTranslations".

## Google integration
If you want to send emails through Gmail (what is coded for now), you will need to download a [client_secret_google_api.json from your Google console] (https://console.cloud.google.com/) and put it in your resources folder.
