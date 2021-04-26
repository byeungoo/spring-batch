package fastcampus.spring.batch.part1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class HelloConfiguration {

    private final JobBuilderFactory jobBuilderFactory;      //스프링 배치는 job을 만들 수 있도록 JobBuilderFactory라는 클래스를 제공. 빈으로 생성되어있음.
    private final StepBuilderFactory stepBuilderFactory;

    //스프링 배치에서 Job은 배치의 실행 단위로 보면 된다.

    @Bean
    public Job helloJob(){
        return jobBuilderFactory.get("helloJob")        //job의 이름
                .incrementer(new RunIdIncrementer())    //job이 실행할 때 마다 파라미터 아이디를 자동으로 생성해주는 클래스. sequence한 파라미터를 자동으로 생성해줌.
                .start(this.helloStep())           //job실행 시 최초로 실행되는 step을 설정하는 메소드.
                .build();
    }


    //step은 job의 실행 단위. 하나의 job은 1개 이상의 step을 가질 수 있음.
    //step도 job처럼 빈으로 만들어야함.
    @Bean
    public Step helloStep(){
        return stepBuilderFactory.get("helloJob")       //step 이름 설정
                .tasklet((contribution, chunkContext) -> {      //tasklet이라는 step의 실행 단위를 설정 해야함
                    log.info("hello spring batch");
                    return RepeatStatus.FINISHED;
                }).build();
    }

}
