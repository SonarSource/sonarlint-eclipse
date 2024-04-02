ARG CIRRUS_AWS_ACCOUNT=275878209202
ARG JDK_VERSION=17
FROM public.ecr.aws/docker/library/eclipse-temurin:11-jammy AS buildphase

FROM ${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j${JDK_VERSION}-m3.9-latest

USER root
COPY --from=buildphase /opt/java/openjdk /opt/java/openjdk-11

# AWS CLI
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm awscliv2.zip

ARG NODE_VERSION=20
RUN apt-get update && apt-get install -y metacity xvfb ffmpeg nodejs=${NODE_VERSION}.* gettext-base \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
COPY --chmod=755 .cirrus/init.d/xvfb /etc/init.d/

USER sonarsource
ENV JAVA_HOME_11_X64=/opt/java/openjdk-11
ENV JAVA_HOME_17_X64=/opt/java/openjdk
COPY --chown=sonarsource .cirrus/toolchains.xml .m2/toolchains.xml
