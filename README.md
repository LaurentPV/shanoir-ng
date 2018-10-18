# About Shanoir-NG (next generation)

GitHub is dedicated to developers, if you search more information from an user's
point of view please see http://shanoir.org.

Shanoir-NG is based on a microservice architecture, that heavily uses Docker.
Each Docker container integrates one microservice, mostly based on Spring Boot.
Each microservice exposes a REST interface on using Swagger 2, as definition format.
The front-end/web interface is implemented on using "Angular 2" (now 5) technology.
Nginx and Keycloak (on using OpenID-Connect) are used to glue everything together.
Internally dcm4che3 is used to handle all DICOM concerns and dcm4chee-arc as backup PACS.
Furthermore dcm2niix is used for the DICOM to NIfTI conversion and Papaya Viewer for DICOM/NIfTI web view.

Many thanks to all these giants: on their shoulders we are standing to develop Shanoir-NG!!!

# Installation of Shanoir-NG

The installation of Shanoir-NG is based on two components:
* BUILD (COMPILE): with Maven 3
* DEPLOY: with docker-compose, version 3

## --------------- BUILD (COMPILE) ---------------

* Install Maven 3 on your machine/on the server
* Get access to the GitHub repository and clone the shanoir-ng repository
* Execute the Maven build on the parent project with the following commands:
    * cd shanoir-ng-parent/
    * **mvn install -DskipTests**
        * the tests will have to be cleaned up soon
* The build creates all .jar and .js executable files and copies them
into the folder /docker-compose to be used from there by docker-compose

## --------------- DEPLOY ------------------------

* Install docker and docker-compose:
    * https://docs.docker.com/install/
    * https://docs.docker.com/compose/install/
* If you are on your **developer/local machine**:
    * Configure your local **/etc/hosts** (for windows, C:/Windows/System32/drivers/etc/hosts) and add:
	* 127.0.0.1       shanoir-ng-nginx
    * For windows 7, increase your RAM and set the port redirection for the virtual box.
* If you are on a **dedicated server** (e.g. shanoir-ng.irisa.fr):
    * By default Shanoir-NG is installed with the host shanoir-ng-nginx and the scheme http (dev setup)
    * If you are on a dedicated server (e.g. shanoir-ng.irisa.fr) you will have to do manual adaptions
      (we tried to automate as much as possible in a given time and there is still a way to go, but here we are currently):
	* 1) Keycloak: Open **/docker-compose/keycloak/cfg/shanoir-ng-realm.json** and change **redirectUris** and **webOrigins**
	* 2) Spring Boot: Open **/.env** and change the host and scheme of all three properties in the file
	* 3) Docker Compose: Open **/docker-compose.yml** and change the **container_name** of Nginx to e.g. shanoir-ng.irisa.fr
	This is necessary, that e.g. ms users and the Keycloak CLI client can access to Keycloak (resolve the host name)
	* 4) Angular: Open **/shanoir-ng-front/config/webpack.config.js** and change **SHANOIR_NG_URL_SCHEME** and **SHANOIR_NG_URL_HOST**
	
    * **Attention:** you will have to re-compile your code after these changes with Maven!!!

* Just in case you have some old stuff of Shanoir-NG in your docker environment:
    * **docker system prune -a**
    * **docker volume prune**
    * **Attention:** this will clean your entire docker system!

* Go to the root folder and execute **docker-compose up --build**

* Access to shanoir-ng: http://shanoir-ng-nginx
By default, new user accounts have been created in Keycloak by ms users. Please access to Keycloak
admin interface below to reset the password, when you want to login (Manage users - Edit your desired
user - Credentials - Reset password and Temporary password: No). When a SMTP server has been configured
properly, emails with a temporary password will have been sent (not the case in dev environment).


* Access to Keycloak admin interface: http://localhost:8080/auth/admin/
* Access to dcm4chee 5 arc-light: http://localhost:8081/dcm4chee-arc/ui2/

* This installation uses Docker named volumes, find more here to handle your local data:
https://docs.docker.com/storage/volumes/
