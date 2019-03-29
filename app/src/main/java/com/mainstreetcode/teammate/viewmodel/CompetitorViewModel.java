package com.mainstreetcode.teammate.viewmodel;

import com.mainstreetcode.teammate.model.Competitor;
import com.mainstreetcode.teammate.model.User;
import com.mainstreetcode.teammate.repository.CompetitorRepo;
import com.mainstreetcode.teammate.repository.RepoProvider;
import com.mainstreetcode.teammate.util.FunctionalDiff;
import com.mainstreetcode.teammate.viewmodel.events.Alert;
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.recyclerview.widget.DiffUtil;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

/**
 * View model for User and Auth
 */

public class CompetitorViewModel extends MappedViewModel<Class<User>, Competitor> {

    private final CompetitorRepo repository;
    private final List<Differentiable> declined = new ArrayList<>();

    public CompetitorViewModel() { repository = RepoProvider.forRepo(CompetitorRepo.class); }

    @Override
    Class<Competitor> valueClass() { return Competitor.class; }

    @Override
    public List<Differentiable> getModelList(Class<User> key) { return declined; }

    public Completable updateCompetitor(Competitor competitor) {
        if (competitor.isEmpty()) return Completable.complete();
        return repository.get(competitor).ignoreElements().observeOn(mainThread());
    }

    public Single<DiffUtil.DiffResult> respond(final Competitor competitor, boolean accept) {
        if (accept) competitor.accept();
        else competitor.decline();
        Single<List<Differentiable>> single = repository.createOrUpdate(competitor).map(Collections::singletonList);
        return FunctionalDiff.of(single, declined, (sourceCopy, fetched) -> {
            if (accept) sourceCopy.removeAll(fetched);
            else sourceCopy.addAll(fetched);

            if (accept) return sourceCopy;

            pushModelAlert(competitor.inOneOffGame()
                    ? Alert.deletion(competitor.getGame())
                    : Alert.deletion(competitor.getTournament()));

            return sourceCopy;
        });
    }

    @Override
    Flowable<List<Competitor>> fetch(Class<User> key, boolean fetchLatest) {
        return repository.getDeclined(getQueryDate(fetchLatest, key, Competitor::getCreated)).toFlowable();
    }
}
