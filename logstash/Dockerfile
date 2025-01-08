FROM docker.elastic.co/logstash/logstash:8.10.1
RUN logstash-plugin install logstash-output-opensearch
COPY logstash.conf /usr/share/logstash/pipeline/logstash.conf
