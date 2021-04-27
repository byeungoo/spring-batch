package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ItemReaderConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job itemReaderJob() throws Exception {
        return this.jobBuilderFactory.get("itemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(this.customItemReaderStep())
                .next(this.csvFileStep())
                .build();
    }

    @Bean
    public Step customItemReaderStep() {
        return this.stepBuilderFactory.get("customItemReaderStep")
                .<Person, Person>chunk(10)
                .reader(new CustomItemReader<>(getItems()))
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Step csvFileStep() throws Exception{
        return stepBuilderFactory.get("csvFileStep")
                .<Person, Person>chunk(10)
                .reader(this.csvFileItemReader())
                .writer(itemWriter())
                .build();
    }

    private FlatFileItemReader<Person> csvFileItemReader() throws Exception{
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();       //csv를 1줄씩읽을 수 있는 객체 생성
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();        //csv 파일을 person 객체에 매핑하기 위해서 토크나이저 객체 생성
        tokenizer.setNames("id", "name", "age", "address");                     //person의 필드명 세팅
        lineMapper.setLineTokenizer(tokenizer);

        lineMapper.setFieldSetMapper(fieldSet -> {      //csv파일에서 읽은 값을 Person 객체로 매핑
            int id = fieldSet.readInt("id");
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");
            return new Person(id, name, age, address);
        });

        FlatFileItemReader<Person> itemReader = new FlatFileItemReaderBuilder<Person>()
                .name("csvFileItemReader")
                .encoding("UTF-8")
                .resource(new ClassPathResource("test.csv"))
                .linesToSkip(1)    //첫번째 ROW는 필드명이니까 2번째부터 읽으라는 설정
                .lineMapper(lineMapper)
                .build();
        itemReader.afterPropertiesSet();    //itemReader에서 필요한 필수설정값이 정상적으로 설정 됐는지 검증하는 메소드

        return itemReader;
    }

    private ItemWriter<Person> itemWriter() {
        return items -> log.info(items.stream()
                .map(Person::getName)
                .collect(Collectors.joining(", ")));
    }

    private List<Person> getItems() {
        List<Person> items = new ArrayList<>();
        for (int i=0; i < 10; i++) {
            items.add(new Person(i+1, "test name" + i, "test age", "test address"));
        }
        return items;
    }

}
