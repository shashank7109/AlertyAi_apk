package com.alertyai.app.ui.teams;

import com.alertyai.app.data.repository.OrgRepository;
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
public final class TeamDashboardViewModel_Factory implements Factory<TeamDashboardViewModel> {
  private final Provider<OrgRepository> repositoryProvider;

  public TeamDashboardViewModel_Factory(Provider<OrgRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TeamDashboardViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TeamDashboardViewModel_Factory create(Provider<OrgRepository> repositoryProvider) {
    return new TeamDashboardViewModel_Factory(repositoryProvider);
  }

  public static TeamDashboardViewModel newInstance(OrgRepository repository) {
    return new TeamDashboardViewModel(repository);
  }
}
