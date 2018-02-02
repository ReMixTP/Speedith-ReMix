FROM maven:3.5-jdk-7
RUN mkdir /speedith/ /speedith-built/
COPY ./ /speedith/
WORKDIR /speedith/
RUN mv local_m2 /root/.m2  # local_m2 contains all the maven dependencies. This lets us build inside Docker without lots of downloading.
RUN mvn install
#RUN mv Speedith.Gui/target/speedith-gui-0.0.1-SNAPSHOT-bin.zip /speedith-built/
RUN mv Speedith.ReMix/target/speedith-remix-0.0.1-SNAPSHOT.jar /speedith-built/
WORKDIR /speedith-built/
#RUN unzip speedith-gui-0.0.1-SNAPSHOT-bin.zip
#ENTRYPOINT ./speedith/bin/main-form-executable-wrapper
ENTRYPOINT java -jar speedith-remix-0.0.1-SNAPSHOT.jar
