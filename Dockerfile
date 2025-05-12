FROM artifactory-service-address/path-to-java-image

LABEL maintainer="our-team@qubership.org"
LABEL atp.service="atp-datasets"

ENV HOME_EX=/service_dataset

WORKDIR $HOME_EX

COPY --chmod=775 dist/atp /atp/
COPY --chown=atp:root build $HOME_EX/

RUN apk add --update --no-cache fontconfig ttf-dejavu && \
    rm -rf /var/cache/apk/* && \
    find $HOME_EX -type f -name '*.sh' -exec chmod a+x {} + && \
    find $HOME_EX -type d -exec chmod 777 {} \;

EXPOSE 8080 9000

USER atp

CMD ["./run.sh"]
