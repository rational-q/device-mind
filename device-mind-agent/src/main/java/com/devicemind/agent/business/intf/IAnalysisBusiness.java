package com.devicemind.agent.business.intf;

import com.devicemind.agent.model.AlertAnalysisRequest;
import com.devicemind.agent.model.AlertAnalysisResponse;
import com.devicemind.agent.model.ChatRequest;
import com.devicemind.agent.model.ChatResponse;
import jakarta.validation.Valid;

public interface IAnalysisBusiness {
    AlertAnalysisResponse analyze(@Valid AlertAnalysisRequest request);

    ChatResponse chat(@Valid ChatRequest request);
}
