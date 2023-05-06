package org.art.service.impl;

import lombok.AllArgsConstructor;
import org.art.dao.RawDataDao;
import org.art.entity.RawData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashSet;
import java.util.Set;

@SpringBootTest
class MainServiceImplTest {

    @Autowired
    private RawDataDao rawDataDao;

    @Test
    public void testSaveData() {
        Message message = new Message();
        message.setText("some text message");

        Update update = new Update();
        update.setMessage(message);

        RawData rawData = RawData.builder()
                .event(update)
                .build();

        Set<RawData> testRawData = new HashSet<>();
        testRawData.add(rawData);

        rawDataDao.save(rawData);

        Assert.isTrue(testRawData.contains(rawData), "Entity not found in the set");

    }

}