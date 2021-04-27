package fastcampus.spring.batch.part3;


import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;

public class CustomItemReader<T> implements ItemReader<T> {

    private final List<T> items;

    public CustomItemReader(List<T> items){
        this.items = new ArrayList<>(items);
    }

    @Override
    public T read() throws Exception, UnexpectedException, ParseException, NonTransientResourceException {
        if(!items.isEmpty()){
            return items.remove(0); //0번째 인덱스 반환하면서 제거
        }
        return null;
    }

}
