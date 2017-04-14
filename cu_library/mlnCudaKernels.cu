extern "C"
__global__ void initDbIndexKernel(int totalVars, int totalPreds, int *d_varDomainSizes,
                                  int *d_predBaseIdx, int *d_predVarMat, int *d_dbIndex,
                                  long totalGroundings)
{
  long idx = blockIdx.x * blockDim.x + threadIdx.x;

  if(idx < totalGroundings)
    {
      long baseDbIndex = idx * totalPreds;
      for(int i = 0; i < totalPreds; i++)
	  d_dbIndex[baseDbIndex + i] = d_predBaseIdx[i];

      long n = idx;
      for(int i = totalVars-1; i >= 0; i--)
	{
	  int domainSize = d_varDomainSizes[i];
	  long temp = n / domainSize;
	  int val = n - temp * domainSize;
	  n = temp;

	  int basePredVarMatIndex = i * totalPreds;
	  for(int j = 0; j < totalPreds; j++)
	      d_dbIndex[baseDbIndex + j] += d_predVarMat[basePredVarMatIndex + j] * val;
	}
    }
}

extern "C"
__global__ void evalClauseKernel(int *d_satArray, int **d_interpretation, int *dbIndex,
                                 int *d_predicates, int *d_valTrue, int totalPreds, long totalGroundings)
{
  long idx = blockIdx.x * blockDim.x + threadIdx.x;

  if(idx < totalGroundings && d_satArray[idx] == 1)
    {
      long baseDbIndex = idx * totalPreds;
      int sat = 0;
      for(int i = 0; i < totalPreds; i++)
	{
	  int predId = d_predicates[i];
	  long interpretationIdx = dbIndex[baseDbIndex + i];
	  sat = max(sat, d_interpretation[predId][interpretationIdx] == d_valTrue[i]);
	}

      d_satArray[idx] = sat;
    }
}
