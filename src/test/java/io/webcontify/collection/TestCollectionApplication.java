package io.webcontify.collection;

import org.springframework.boot.SpringApplication;

public class TestCollectionApplication {

	public static void main(String[] args) {
		SpringApplication.from(CollectionApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
