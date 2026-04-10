"use client";

import { useForm, type Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/Button";
import { createAlertRule } from "@/lib/api/alerts";
import type { CreateAlertRuleInput, VerdictTrigger } from "@/types/api";

const schema = z
  .object({
    userIdentifier: z.string().min(2, "2자 이상 입력해주세요"),
    departureDateFrom: z.string().min(1, "출발일 시작을 선택해주세요"),
    departureDateTo: z.string().min(1, "출발일 종료를 선택해주세요"),
    targetPrice: z
      .string()
      .optional()
      .refine(
        (v) => !v || (/^\d+$/.test(v) && Number(v) > 0),
        "양의 정수를 입력해주세요",
      ),
    verdictTrigger: z.enum(["CHEAP", "CHEAP_OR_FAIR"]),
  })
  .refine((v) => v.departureDateFrom <= v.departureDateTo, {
    message: "종료일은 시작일 이후여야 해요",
    path: ["departureDateTo"],
  });

type FormValues = z.infer<typeof schema>;

interface AlertRuleFormProps {
  routeId: number;
}

export function AlertRuleForm({ routeId }: AlertRuleFormProps) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema) as unknown as Resolver<FormValues>,
    defaultValues: {
      userIdentifier: "",
      departureDateFrom: "",
      departureDateTo: "",
      targetPrice: "",
      verdictTrigger: "CHEAP",
    },
  });

  const mutation = useMutation({
    mutationFn: (input: CreateAlertRuleInput) => createAlertRule(input),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["alertRules", routeId] });
      reset();
    },
  });

  const onSubmit = (values: FormValues) => {
    const targetPrice =
      values.targetPrice && values.targetPrice.length > 0
        ? Number(values.targetPrice)
        : null;
    mutation.mutate({
      routeId,
      userIdentifier: values.userIdentifier,
      departureDateFrom: values.departureDateFrom,
      departureDateTo: values.departureDateTo,
      targetPrice,
      verdictTrigger: values.verdictTrigger as VerdictTrigger,
    });
  };

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      className="flex flex-col gap-4"
      noValidate
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="사용자 식별자" error={errors.userIdentifier?.message}>
          <input
            type="text"
            placeholder="you@example.com"
            className="form-input"
            {...register("userIdentifier")}
          />
        </Field>
        <Field label="판단 트리거" error={errors.verdictTrigger?.message}>
          <select className="form-input" {...register("verdictTrigger")}>
            <option value="CHEAP">지금 사세요(CHEAP)만 알림</option>
            <option value="CHEAP_OR_FAIR">적정가(FAIR)까지 알림</option>
          </select>
        </Field>
        <Field label="출발일 (시작)" error={errors.departureDateFrom?.message}>
          <input
            type="date"
            className="form-input"
            {...register("departureDateFrom")}
          />
        </Field>
        <Field label="출발일 (종료)" error={errors.departureDateTo?.message}>
          <input
            type="date"
            className="form-input"
            {...register("departureDateTo")}
          />
        </Field>
        <Field
          label="목표 가격 (선택)"
          error={errors.targetPrice?.message}
        >
          <input
            type="number"
            placeholder="예: 250000"
            className="form-input"
            {...register("targetPrice")}
          />
        </Field>
      </div>

      {mutation.isError ? (
        <p className="text-sm text-[var(--color-danger)]">
          {mutation.error instanceof Error
            ? mutation.error.message
            : "알림 등록에 실패했어요"}
        </p>
      ) : null}
      {mutation.isSuccess ? (
        <p className="text-sm text-[var(--color-success)]">
          알림 룰이 등록됐어요
        </p>
      ) : null}

      <div>
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "등록 중..." : "알림 등록하기"}
        </Button>
      </div>
    </form>
  );
}

interface FieldProps {
  label: string;
  error?: string;
  children: React.ReactNode;
}

function Field({ label, error, children }: FieldProps) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-semibold text-[var(--color-text-secondary)]">
        {label}
      </span>
      {children}
      {error ? (
        <span className="text-xs text-[var(--color-danger)]">{error}</span>
      ) : null}
    </label>
  );
}
