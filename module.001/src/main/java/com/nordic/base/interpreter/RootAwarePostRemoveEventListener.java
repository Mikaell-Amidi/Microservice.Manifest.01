package com.nordic.base.interpreter;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.persister.entity.EntityPersister;


@Slf4j
@RequiredArgsConstructor
public class RootAwarePostRemoveEventListener implements PostDeleteEventListener {

    @Override
    public void onPostDelete(PostDeleteEvent postDeleteEvent) {
        //....
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister entityPersister) {
        return false;
    }

}