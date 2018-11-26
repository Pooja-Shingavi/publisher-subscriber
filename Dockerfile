From tomcat:8-jre8

# Maintainer
MAINTAINER "Rajkushal <rajkusha@buffalo.edu">
EXPOSE 8090
ADD phase2_2.war /usr/local/tomcat/webapps/.
RUN sed -i 's/port="8080"/port="8090"/' /usr/local/tomcat/conf/server.xml




