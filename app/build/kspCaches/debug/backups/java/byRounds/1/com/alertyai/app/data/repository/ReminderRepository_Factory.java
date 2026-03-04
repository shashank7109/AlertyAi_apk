package com.alertyai.app.data.repository;

import com.alertyai.app.data.local.ReminderDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ReminderRepository_Factory implements Factory<ReminderRepository> {
  private final Provider<ReminderDao> daoProvider;

  public ReminderRepository_Factory(Provider<ReminderDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public ReminderRepository get() {
    return newInstance(daoProvider.get());
  }

  public static ReminderRepository_Factory create(Provider<ReminderDao> daoProvider) {
    return new ReminderRepository_Factory(daoProvider);
  }

  public static ReminderRepository newInstance(ReminderDao dao) {
    return new ReminderRepository(dao);
  }
}
