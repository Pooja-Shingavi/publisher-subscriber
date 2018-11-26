#Phase3 of the project will have 3 docket containers deployed, 
#one with a pub-sub system from phase2.war, second with a pub-sub system from phase2_2.war and third with a mySQL db

1. Install docker
2. Copy phase2.war from dir-container1 file to new dir (Phase2) directory
3. Place Dockerfile from dir-container1 renaming it to Dockerfile into Phase2 dir
4. Build docker image-"web1" by executing below command from Phase2 dir
docker build -t web1 .
5. Copy phase2_2.war from dir-container2 file to new dir (Phase2_2) directory
6. Place Dockerfile from dir-container2 renaming it to Dockerfile into Phase2_2 dir
7. Build docker image-"web2" by executing below command from Phase2_2 dir
docker build -t web2 .
8. Run docker image mysql - docker run  --net=my-network -p 3306:3306 --name mysql -e MYSQL_ROOT_PASSWORD=Pooja@007 mysql
9. Access running mysql container by - mysql -h localhost -P 3306 --protocol=tcp -u root -p
10. Run the schema.sql script to setup the database schema
11. Run docker image web1 - docker run -it --rm -p 8080:8080 web1
12. Run docker image web2 - docker run -it --rm -p 8090:8090 web2
13. Run command- docker network create my-network
14. Access web1 Publisher at 127.0.0.1:8080/p.html, and Subscriber at 127.0.0.1:8080/s.html
15. Access web2 Publisher at 127.0.0.1:8090/p.html, and Subscriber at 127.0.0.1:8090/s.html
16. Add New Publisher Name and New Topic on either of p.html
17. Add New Subscriber at either of s.html
18. For basic verification, Publish a message from Publisher and check output on Subscriber page on each of s.html
19. Add multiple Topics and Subscribers for Full testing of all Publisher-Consumer scenarios on both hosted systems
20. Delete subscribers from either s.html, and verify publishing on topics it was prior subscribed to
