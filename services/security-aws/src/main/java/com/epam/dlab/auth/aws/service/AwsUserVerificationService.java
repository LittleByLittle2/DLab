package com.epam.dlab.auth.aws.service;

import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.User;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.aws.dao.AwsUserDAO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class AwsUserVerificationService implements UserVerificationService {

	private final AwsUserDAO awsUserDAO;

	@Inject
	public AwsUserVerificationService(AwsUserDAO awsUserDAO) {
		this.awsUserDAO = awsUserDAO;
	}

	@Override
	public void verify(String username, UserInfo userInfo) {
		verifyAwsUser(username, userInfo);
		verifyAwsKeys(username, userInfo);
	}


	private User verifyAwsUser(String username, UserInfo userInfo) {
		try {
			User awsUser = awsUserDAO.getAwsUser(username);
			if (awsUser != null) {
				userInfo.setAwsUser(true);
				return awsUser;
			} else {
				throw new DlabException("Please contact AWS administrator to create corresponding IAM User");
			}
		} catch (RuntimeException e) {
			throw new DlabException("Please contact AWS administrator to create corresponding IAM User", e);
		}
	}

	private List<AccessKeyMetadata> verifyAwsKeys(String username, UserInfo userInfo) {

		userInfo.getKeys().clear();

		try {
			List<AccessKeyMetadata> keys = awsUserDAO.getAwsAccessKeys(username);
			if (keys == null || keys.isEmpty()
					|| keys.stream().noneMatch(k -> "Active".equalsIgnoreCase(k.getStatus()))) {

				throw new DlabException("Cannot get aws access key for user " + username);
			}
			keys.forEach(e -> userInfo.addKey(e.getAccessKeyId(), e.getStatus()));

			return keys;
		} catch (RuntimeException e) {
			throw new DlabException("Please contact AWS administrator to activate your Access Key", e);
		}
	}
}
