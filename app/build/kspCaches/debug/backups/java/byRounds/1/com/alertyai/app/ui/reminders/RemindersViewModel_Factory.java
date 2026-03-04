package com.alertyai.app.ui.reminders;

import com.alertyai.app.data.repository.ReminderRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class RemindersViewModel_Factory implements Factory<RemindersViewModel> {
  private final Provider<ReminderRepository> repoProvider;

  public RemindersViewModel_Factory(Provider<ReminderRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public RemindersViewModel get() {
    return newInstance(repoProvider.get());
  }

  public static RemindersViewModel_Factory create(Provider<ReminderRepository> repoProvider) {
    return new RemindersViewModel_Factory(repoProvider);
  }

  public static RemindersViewModel newInstance(ReminderRepository repo) {
    return new RemindersViewModel(repo);
  }
}
