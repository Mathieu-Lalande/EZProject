package com.ezproject.report;

import java.time.LocalDate;
import java.util.List;

import com.ezproject.project.Health;
import com.ezproject.project.ProjectDto;
import com.ezproject.retroplanning.RetroPlanDto;
import com.ezproject.risk.RiskDtos.DecisionDto;
import com.ezproject.risk.RiskDtos.RiskDto;
import com.ezproject.time.TimeDtos.SummaryDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record ReportRequest(@NotNull Health health, @NotNull Trend trend,
                                @NotBlank @Size(max = 2000) String summary, @Size(max = 2000) String achievements,
                                @Size(max = 2000) String nextSteps, @Size(max = 2000) String attentionPoints) {
    }

    public record ReportDto(Long id, LocalDate reportDate, Health health, Trend trend, String summary,
                            String achievements, String nextSteps, String attentionPoints, String authorName) {

        static ReportDto from(StatusReport r) {
            return new ReportDto(r.getId(), r.getReportDate(), r.getHealth(), r.getTrend(), r.getSummary(),
                    r.getAchievements(), r.getNextSteps(), r.getAttentionPoints(),
                    r.getAuthor() == null ? null : r.getAuthor().getDisplayName());
        }
    }

    public record TaskStats(long total, long done, long overdue, int progress) {
    }

    /**
     * Rapport COPIL : tout ce qu'il faut pour un comité, selon les droits du lecteur
     * (les blocs non autorisés sont null).
     */
    public record CopilDto(ProjectDto project, ReportDto latestReport, List<ReportDto> history, TaskStats tasks,
                           RetroPlanDto plan, List<RiskDto> risks, List<DecisionDto> decisions, SummaryDto budget,
                           LocalDate generatedOn) {
    }
}
