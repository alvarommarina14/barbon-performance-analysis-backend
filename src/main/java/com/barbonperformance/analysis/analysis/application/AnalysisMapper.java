package com.barbonperformance.analysis.analysis.application;

import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisDetail;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisSummary;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.FindingView;
import com.barbonperformance.analysis.analysis.domain.Analysis;
import com.barbonperformance.analysis.analysis.domain.AnalysisFinding;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import com.barbonperformance.analysis.video.domain.VideoSubmission;

final class AnalysisMapper {

    private AnalysisMapper() {
    }

    static AnalysisDetail detail(Analysis analysis) {
        VideoSubmission submission = analysis.getSubmission();
        VideoAsset feedback = analysis.getFeedbackVideoAsset();
        return new AnalysisDetail(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getTitle(),
                analysis.getSummary(),
                analysis.getPlayer().getId(),
                analysis.getPlayer().getUser().getName(),
                analysis.getCoach().getId(),
                analysis.getCoach().getUser().getName(),
                submission == null ? null : submission.getId(),
                submission == null ? null : submission.getPlayerNote(),
                feedback == null ? null : feedback.getId(),
                feedback == null ? null : feedback.getStatus(),
                analysis.feedbackReady(),
                analysis.getPublishedAt(),
                analysis.getFindings().stream().map(AnalysisMapper::finding).toList(),
                analysis.getCreatedAt(),
                analysis.getUpdatedAt());
    }

    static AnalysisSummary summary(Analysis analysis) {
        VideoSubmission submission = analysis.getSubmission();
        return new AnalysisSummary(
                analysis.getId(),
                analysis.getTitle(),
                analysis.getStatus(),
                analysis.getCoach().getUser().getName(),
                analysis.getPlayer().getUser().getName(),
                submission == null ? null : submission.getId(),
                analysis.feedbackReady(),
                analysis.getPublishedAt(),
                analysis.getFindings().size(),
                analysis.getCreatedAt());
    }

    private static FindingView finding(AnalysisFinding finding) {
        return new FindingView(
                finding.getId(),
                finding.getTimestampSeconds(),
                finding.getTitle(),
                finding.getDetail(),
                finding.getSortOrder());
    }
}
