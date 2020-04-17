%% Resynthesize the EGG waveshapes from a [levels, phases] array.
% NOTE: in the _clusters.csv file, levels are given in Bels, not in dB!
% To extend the code to different selected data or a different text file,
% generate a function instead of a script.

function [egg, timeStruct] = synthEGGfromArrays(levels, phases, nHarmonics, points, periods)
    actualAmps = power(10, levels(1:nHarmonics)/2.0);
    actualPhases = phases(1:nHarmonics);
    nPeriod = points;
    nStep = 2*pi/nPeriod;
    
    timeStruct.qC = 0.5;
    timeStruct.maxDegg = 1.0;
    timeStruct.p2pAmpl = 1.0;
    timeStruct.iC = 0.0;

    %Compute periods of the waveshapes 
    harmonics = zeros(nHarmonics, round(periods*nPeriod));
    for k = 1:nHarmonics
        for i = 1:(round(periods*nPeriod))
            harmonics(k, i) = actualAmps(k) * cos((i-1)*(k*nStep) + actualPhases(k)); 
        end
    end
    wave = sum(harmonics);
    aMax = max(wave);
    aMin = min(wave);
    timeStruct.p2pAmpl = aMax - aMin;
    wave = wave(:) ./ (aMax-aMin);
    aMin = min(wave);
    timeStruct.qC = sum((wave(1:nPeriod) - aMin)) / nPeriod;
    wPeriod = wave(1:nPeriod+1);
    wPeriodDiff = wave(2:nPeriod+1) - wave(1:nPeriod);
    timeStruct.maxDegg = max(wPeriodDiff)/(0.5*sin(2*pi/points));
    timeStruct.iC = timeStruct.qC * log10(timeStruct.maxDegg);
    egg = wave;
end

