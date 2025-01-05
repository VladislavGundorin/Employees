package org.example;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import org.example.grpc.EmployeeServiceGrpcImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Main {
    public static void main(String[] args) throws Exception {
        ApplicationContext context = SpringApplication.run(Main.class, args);

        EmployeeServiceGrpcImpl grpcService = context.getBean(EmployeeServiceGrpcImpl.class);

        Server server = NettyServerBuilder.forPort(9091)
                .addService(grpcService)
                .build()
                .start();

        server.awaitTermination();
    }
}
