package com.skillsense.survey.service;

import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.survey.model.SurveyCategory;
import com.skillsense.survey.model.TargetFilter;
import com.skillsense.user.Role;
import com.skillsense.user.User;
import com.skillsense.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TargetingService {

    private final UserRepository userRepository;

    public TargetingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public TargetFilter normalizeAndValidateFilter(TargetFilter filter, User caller, SurveyCategory category) {
        if (filter == null) {
            filter = new TargetFilter("ALL", new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        Role role = caller.getRole();

        if (role == Role.HOD) {
            // Section 2 & 6: A HOD's target filter is forced to their own branch by the backend, whatever the client sends
            String hodBranch = caller.getBranch() != null ? caller.getBranch() : "CSE";
            filter.setScope("FILTER");
            filter.setBranches(new ArrayList<>(Collections.singletonList(hodBranch)));
            return filter;
        }

        if (role == Role.FACULTY) {
            // Section 2 & Prompt: Faculty cannot target college-wide, must target own branch only
            if ("ALL".equalsIgnoreCase(filter.getScope()) || filter.getBranches() == null || filter.getBranches().isEmpty()) {
                throw new ApiException(
                        HttpStatus.FORBIDDEN,
                        ErrorCode.FORBIDDEN,
                        "Faculty cannot target college-wide; target must specify branch"
                );
            }
            if (caller.getBranch() != null && !caller.getBranch().trim().isEmpty()) {
                for (String branch : filter.getBranches()) {
                    if (!caller.getBranch().equalsIgnoreCase(branch)) {
                        throw new ApiException(
                                HttpStatus.FORBIDDEN,
                                ErrorCode.FORBIDDEN,
                                "Faculty can only target their own branch (" + caller.getBranch() + ")"
                        );
                    }
                }
            }
        }

        return filter;
    }

    public List<User> findEligibleStudents(TargetFilter filter) {
        if (filter == null || "ALL".equalsIgnoreCase(filter.getScope())) {
            return userRepository.findEligibleStudents(false, Collections.emptyList(), false, Collections.emptyList(), false, Collections.emptyList());
        }

        boolean useBranches = filter.getBranches() != null && !filter.getBranches().isEmpty();
        List<String> branches = useBranches ? filter.getBranches() : Collections.emptyList();

        boolean useYears = filter.getYears() != null && !filter.getYears().isEmpty();
        List<Integer> years = useYears ? filter.getYears() : Collections.emptyList();

        boolean useSections = filter.getSections() != null && !filter.getSections().isEmpty();
        List<String> sections = useSections ? filter.getSections() : Collections.emptyList();

        return userRepository.findEligibleStudents(useBranches, branches, useYears, years, useSections, sections);
    }

    public long countEligibleStudents(TargetFilter filter) {
        if (filter == null || "ALL".equalsIgnoreCase(filter.getScope())) {
            return userRepository.countEligibleStudents(false, Collections.emptyList(), false, Collections.emptyList(), false, Collections.emptyList());
        }

        boolean useBranches = filter.getBranches() != null && !filter.getBranches().isEmpty();
        List<String> branches = useBranches ? filter.getBranches() : Collections.emptyList();

        boolean useYears = filter.getYears() != null && !filter.getYears().isEmpty();
        List<Integer> years = useYears ? filter.getYears() : Collections.emptyList();

        boolean useSections = filter.getSections() != null && !filter.getSections().isEmpty();
        List<String> sections = useSections ? filter.getSections() : Collections.emptyList();

        return userRepository.countEligibleStudents(useBranches, branches, useYears, years, useSections, sections);
    }
}
