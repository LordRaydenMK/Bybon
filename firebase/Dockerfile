FROM node:26-trixie-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl \
        openjdk-21-jre-headless \
    && rm -rf /var/lib/apt/lists/*

RUN npm install -g firebase-tools@14.27.0

WORKDIR /firebase
COPY firebase.json firestore.rules .firebaserc ./

# Download emulator JARs at build time so CI startup does not hit the network.
RUN firebase setup:emulators:firestore

EXPOSE 4400 4500 8080 9099 9150

ENV FIREBASE_PROJECT=demo-bybon

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=20 \
    CMD curl -sf http://127.0.0.1:4400/emulators | grep -q '"auth"' \
        && curl -sf http://127.0.0.1:4400/emulators | grep -q '"firestore"' \
        || exit 1

CMD ["firebase", "emulators:start", "--project", "demo-bybon", "--only", "auth,firestore"]
