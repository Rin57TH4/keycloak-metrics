/**
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.keycloak;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public final class MetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final MeterRegistry meterRegistry = MeterRegistryHolder.getInstance().getMeterRegistry();
    private static final String MICROMETER_TIMER_SAMPLE = "__micrometer_sample";
    private static final MetricsFilter INSTANCE = new MetricsFilter();

    public static MetricsFilter instance() {
        return INSTANCE;
    }

    private MetricsFilter() {
    }

    @Override
    public void filter(ContainerRequestContext req) {
        req.setProperty(MICROMETER_TIMER_SAMPLE, Timer.start(meterRegistry));
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        Timer.Sample sample = (Timer.Sample) req.getProperty(MICROMETER_TIMER_SAMPLE);
        if (sample != null) {
            sample.stop(meterRegistry.timer("http.server.requests", Tags.of(
                    Tag.of("uri", req.getUriInfo().getPath()),
                    Tag.of("method", req.getMethod().toUpperCase()),
                    Tag.of("status", Integer.toString(res.getStatus())),
                    Outcome.forStatus(res.getStatus()).asTag(),
                    Tag.of("content.type", getMediaType(res))
            )));
        }
    }

    private String getMediaType(ContainerResponseContext res) {
        if (res.getMediaType() != null)
            return res.getMediaType().getType();
        return "N/A";
    }
}
