ARG CIRRUS_AWS_ACCOUNT=275878209202
ARG JDK_VERSION=17
FROM public.ecr.aws/docker/library/eclipse-temurin:11-jammy AS buildphase-11
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jammy AS buildphase-21

FROM ${CIRRUS_AWS_ACCOUNT}.dkr.ecr.eu-central-1.amazonaws.com/base:j${JDK_VERSION}-m3.9-latest

USER root
COPY --from=buildphase-11 /opt/java/openjdk /opt/java/openjdk-11
COPY --from=buildphase-21 /opt/java/openjdk /opt/java/openjdk-21

# AWS CLI
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm awscliv2.zip

# Node.js for JS/TS/CSS analysis
# "build-essential" (containing gcc/g++/make) for C/C++ analysis
# Webkit libraries for rendering the SonarQube Rule Description view
ARG NODE_VERSION=22
RUN apt-get update && apt-get install -y \
    build-essential \
    dbus-x11 \
    ffmpeg \
    gettext-base \
    libwebkit2gtk-4.* \
    metacity \
    nodejs=${NODE_VERSION}.* \
    xvfb \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
COPY --chmod=755 .cirrus/init.d/xvfb /etc/init.d/

USER sonarsource
ENV JAVA_HOME_11_X64=/opt/java/openjdk-11
ENV JAVA_HOME_17_X64=/opt/java/openjdk
ENV JAVA_HOME_21_X64=/opt/java/openjdk-21
COPY --chown=sonarsource .cirrus/toolchains.xml .m2/toolchains.xml
# Fix for authenticated access to Reddeer mirror on Repox
COPY --chown=sonarsource .cirrus/settings-public-auth.xml .m2/settings-public-auth.xml
