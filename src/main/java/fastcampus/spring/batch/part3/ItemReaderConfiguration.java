package fastcampus.spring.batch.part3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ItemReaderConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job itemReaderJob() throws Exception {
        return this.jobBuilderFactory.get("itemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(this.customItemReaderStep())
                .next(this.csvFileStep())
                .next(this.jdbcStep())
                .next(this.jpaStep())
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

    private JdbcCursorItemReader<Person> jdbcCursorItemReader() throws Exception {
        JdbcCursorItemReader<Person> itemReader = new JdbcCursorItemReaderBuilder<Person>()
                .name("jdbcCursorItemReader")
                .dataSource(dataSource)
                .sql("select id, name, age, address from person")
                .rowMapper((rs, rowNum) -> new Person(rs.getInt(1), rs.getString(2)
                        , rs.getString(3), rs.getString(4)))
                .build();
        itemReader.afterPropertiesSet();
        return itemReader;
    }

    @Bean
    public Step jdbcStep() throws Exception{
        return stepBuilderFactory.get("jdbcStep")
                .<Person, Person>chunk(10)
                .reader(jdbcCursorItemReader())
                .writer(itemWriter())
                .build();
    }

    @Bean
    public Step jpaStep() throws Exception{
        return stepBuilderFactory.get("jpaStep")
                .<Person, Person>chunk(10)
                .reader(this.jpaCursorItemReader())
                .writer(itemWriter())
                .build();
    }

    private JpaCursorItemReader<Person> jpaCursorItemReader() throws Exception{
        JpaCursorItemReader<Person> itemReader = new JpaCursorItemReaderBuilder<Person>()
                .name("jpaCursorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select p from Person p")
                .build();
        itemReader.afterPropertiesSet();
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
