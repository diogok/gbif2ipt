FROM diogok/java8:zulu

WORKDIR /opt
CMD ["java","-server","-XX:+UseConcMarkSweepGC","-XX:+UseCompressedOops","-XX:+DoEscapeAnalysis","-jar","gbif2ipt.jar"]

ADD target/gbif2ipt-0.0.1-standalone.jar /opt/gbif2ipt.jar

