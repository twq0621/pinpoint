/*
 * Copyright 2019 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.collector.receiver.grpc;

import com.navercorp.pinpoint.collector.receiver.DispatchHandler;
import com.navercorp.pinpoint.collector.receiver.grpc.service.StatService;
import com.navercorp.pinpoint.common.server.util.AddressFilter;
import com.navercorp.pinpoint.common.util.Assert;
import com.navercorp.pinpoint.grpc.server.MetadataServerTransportFilter;
import com.navercorp.pinpoint.grpc.server.ServerFactory;
import com.navercorp.pinpoint.grpc.server.TransportMetadataFactory;
import io.grpc.BindableService;
import com.navercorp.pinpoint.grpc.server.ServerOption;
import io.grpc.Server;
import io.grpc.ServerTransportFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;
import java.util.concurrent.Executor;

public class StatServer implements InitializingBean, DisposableBean, BeanNameAware {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String beanName;
    private boolean enable;

    private String bindIp;
    private int bindPort;

    private ServerFactory serverFactory;
    private Executor executor;

    private DispatchHandler dispatchHandler;
    private AddressFilter addressFilter;
    private ServerOption serverOption;

    private Server server;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (Boolean.FALSE == this.enable) {
            return;
        }

        Assert.requireNonNull(this.beanName, "beanName must not be null");
        Assert.requireNonNull(this.bindIp, "bindIp must not be null");
        Assert.requireNonNull(this.dispatchHandler, "dispatchHandler must not be null");
        Assert.requireNonNull(this.addressFilter, "addressFilter must not be null");


        this.serverFactory = new ServerFactory(beanName, this.bindIp, this.bindPort, this.executor);
        ServerTransportFilter permissionServerTransportFilter = new PermissionServerTransportFilter(addressFilter);
        this.serverFactory.addTransportFilter(permissionServerTransportFilter);

        // Add options

        // Add service
        BindableService statService = new StatService(this.dispatchHandler);
        serverFactory.addService(statService);

        this.server = serverFactory.build();
        if (logger.isInfoEnabled()) {
            logger.info("Start stat server {}", this.server);
        }
        this.server.start();
    }

    @Override
    public void destroy() throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("Destroy stat server {}", this.server);
        }

        if (this.server != null) {
            this.server.shutdown();
        }
        if (this.serverFactory != null) {
            this.serverFactory.close();
        }
    }

    // Test only
    void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    @Override
    public void setBeanName(final String beanName) {
        this.beanName = beanName;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void setBindIp(String bindIp) {
        this.bindIp = bindIp;
    }

    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
    }

    public void setDispatchHandler(DispatchHandler dispatchHandler) {
        this.dispatchHandler = dispatchHandler;
    }

    public void setAddressFilter(AddressFilter addressFilter) {
        this.addressFilter = addressFilter;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setServerOption(ServerOption serverOption) {
        this.serverOption = serverOption;
    }
}